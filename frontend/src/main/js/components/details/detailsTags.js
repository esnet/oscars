import React, { Component } from "react";
import {
    Card,
    CardHeader,
    CardBody,
    Button,
    Input,
    Form,
    InputGroup
} from "reactstrap";
import { observer, inject } from "mobx-react";
import { action } from "mobx";
import { size } from "lodash-es";

import Select from "react-select-plus";
import BootstrapTable from "react-bootstrap-table-next";
import Octicon from "react-octicon";

import myClient from "../../agents/client";

@inject("tagStore", "connsStore")
@observer
class DetailsTags extends Component {
    constructor(props) {
        super(props);
    }

    componentWillMount() {
        this.refreshTagCategories();
    }

    componentWillUnmount() {
        clearTimeout(this.tagUpdateTimeout);
    }

    refreshTagCategories = () => {
        myClient.submitWithToken("GET", "/protected/tag/categories").then(
            action(response => {
                let parsed = JSON.parse(response);
                this.props.tagStore.updateCategories(parsed);
            })
        );
        this.tagUpdateTimeout = setTimeout(this.refreshTagCategories, 20000);
    };

    refreshTags = () => {
        this.props.connsStore.refreshCurrent();
    };

    // key presses
    handleKeyPress = e => {
        const edit = this.props.tagStore.editTag;
        if (e.key === "Enter" && size(edit.category) > 0 && size(edit.contents)) {
            this.add();
        }
    };

    onCategoryChange = val => {
        this.props.tagStore.setEditedTagCtg(val);

        let source = this.props.tagStore.editTag.source;
        if (source != null && source.startsWith("http")) {
            this.fetchContentOptions();
        }
    };

    fetchContentOptions = () => {
        let source = this.props.tagStore.editTag.source;
        if (!source.startsWith("https://spreadsheets.google.com/feeds/cells/")) {
            console.log("invalid source; only google sheets for now");
            this.props.tagStore.setEditedTagContentOptions(["Error!"]);
            return;
        }
        myClient.loadJSON({ method: "GET", url: source }).then(
            action(response => {
                let parsed = JSON.parse(response);
                let entries = parsed["feed"]["entry"];
                let opts = [];
                entries.map(e => {
                    opts.push(e["content"]["$t"]);
                });
                this.props.tagStore.setEditedTagContentOptions(opts);
            })
        );
    };

    onContentsChange = val => {
        this.props.tagStore.setEditedTagContents(val);
    };

    delete = id => {
        const connId = this.props.connsStore.store.current.connectionId;
        myClient.submitWithToken("GET", "/protected/tag/delete/" + connId + "/" + id).then(
            action(() => {
                this.refreshTags();
            })
        );
    };

    add = () => {
        const connId = this.props.connsStore.store.current.connectionId;
        const edit = this.props.tagStore.editTag;
        let tag = {
            id: null,
            category: edit.category,
            contents: edit.contents
        };

        myClient.submitWithToken("POST", "/protected/tag/add/" + connId, tag).then(
            action(() => {
                this.refreshTags();
            })
        );
    };

    idFormatter = (cell, row) => {
        return (
            <Octicon
                name="trashcan"
                onClick={() => this.delete(row.id)}
                className="float-right"
                style={{ height: "16px", width: "16px" }}
            />
        );
        /* we don't need confirmations
        return <ConfirmModal
            body='This will delete the tag.'
            header='Delete tag'
            uiElement={<Octicon name='trashcan' className='float-right' style={{height: '16px', width: '16px'}}/>}
            onConfirm={() => this.delete(row.id)}/>
            */
    };

    render() {
        const edit = this.props.tagStore.editTag;

        let tags = [];
        if (this.props.connsStore.store.current.tags != null) {
            this.props.connsStore.store.current.tags.map(t => {
                tags.push({
                    id: t.id,
                    contents: t.contents,
                    category: t.category
                });
            });
        }
        let categoryOpts = [];
        this.props.tagStore.store.categories.map(c => {
            categoryOpts.push({
                value: c.category,
                label: c.category
            });
        });

        const columns = [
            {
                dataField: "category",
                text: "Category"
            },
            {
                dataField: "contents",
                text: "Contents"
            },
            {
                dataField: "id",
                text: "?",
                formatter: this.idFormatter,
                headerStyle: { width: "40px", textAlign: "center" }
            }
        ];
        const ctgValue = edit.category;
        const contentsValue = edit.contents;

        let ctgSource = this.props.tagStore.editTag.source;
        let contentsInput = (
            <Input
                type="text"
                id="contents"
                bsSize="sm"
                style={{ width: "160px" }}
                placeholder="Contents"
                onKeyPress={this.handleKeyPress}
                onChange={e => this.onContentsChange(e.target.value)}
            />
        );
        if (ctgSource != null && ctgSource !== "" && ctgSource.startsWith("http")) {
            let contentsOpts = [];
            this.props.tagStore.editTag.contentOptions.map(opt => {
                contentsOpts.push({
                    value: opt,
                    label: opt
                });
            });

            contentsInput = (
                <small>
                    <Select
                        style={{ width: "160px" }}
                        name="select-contents"
                        placeholder="Choose.."
                        simpleValue
                        value={contentsValue}
                        onBlurResetsInput={false}
                        onChange={this.onContentsChange}
                        autosize={false}
                        options={contentsOpts}
                    />
                </small>
            );
        }

        return (
            <Card>
                <CardHeader>Tags</CardHeader>
                <CardBody>
                    <BootstrapTable condensed keyField="id" data={tags} columns={columns} />

                    <Form
                        inline
                        onSubmit={e => {
                            e.preventDefault();
                        }}
                    >
                        <InputGroup className="mb-2 mr-sm-2 mb-sm-0">
                            <small>
                                <Select
                                    style={{ width: "100px" }}
                                    name="select-category"
                                    placeholder="Category"
                                    simpleValue
                                    onBlurResetsInput={false}
                                    onChange={this.onCategoryChange}
                                    value={ctgValue}
                                    autosize={false}
                                    options={categoryOpts}
                                />
                            </small>
                            {contentsInput}

                            <Button
                                className="float-right btn-sm"
                                disabled={
                                    size(edit.category) === 0 || size(edit.contents) === 0
                                }
                                onClick={this.add}
                            >
                                Add
                            </Button>
                        </InputGroup>
                    </Form>
                </CardBody>
            </Card>
        );
    }
}

export default DetailsTags;
